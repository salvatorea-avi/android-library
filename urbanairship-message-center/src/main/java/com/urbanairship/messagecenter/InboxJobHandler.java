/* Copyright Airship and Contributors */

package com.urbanairship.messagecenter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.urbanairship.Logger;
import com.urbanairship.PreferenceDataStore;
import com.urbanairship.UAirship;
import com.urbanairship.channel.AirshipChannel;
import com.urbanairship.config.AirshipRuntimeConfig;
import com.urbanairship.http.RequestException;
import com.urbanairship.http.Response;
import com.urbanairship.job.JobInfo;
import com.urbanairship.json.JsonList;
import com.urbanairship.json.JsonValue;
import com.urbanairship.util.UAStringUtil;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Job handler for {@link Inbox} component.
 */
class InboxJobHandler {

    /**
     * Starts the service in order to update just the {@link Message}'s messages.
     */
    static final String ACTION_RICH_PUSH_MESSAGES_UPDATE = "ACTION_RICH_PUSH_MESSAGES_UPDATE";

    /**
     * Starts the service to sync message state.
     */
    static final String ACTION_SYNC_MESSAGE_STATE = "ACTION_SYNC_MESSAGE_STATE";

    /**
     * Starts the service in order to update just the {@link User} itself.
     */
    static final String ACTION_RICH_PUSH_USER_UPDATE = "ACTION_RICH_PUSH_USER_UPDATE";

    /**
     * Extra key to indicate if the rich push user needs to be updated forcefully.
     */
    static final String EXTRA_FORCEFULLY = "EXTRA_FORCEFULLY";

    static final String LAST_MESSAGE_REFRESH_TIME = "com.urbanairship.user.LAST_MESSAGE_REFRESH_TIME";

    private static final String LAST_UPDATE_TIME = "com.urbanairship.user.LAST_UPDATE_TIME";
    private static final long USER_UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000; //24H

    private final MessageCenterResolver resolver;
    private final User user;
    private final Inbox inbox;
    private final PreferenceDataStore dataStore;
    private final AirshipChannel channel;

    private final InboxApiClient inboxApiClient;

    InboxJobHandler(@NonNull Context context,
                    @NonNull Inbox inbox,
                    @NonNull User user,
                    @NonNull AirshipChannel channel,
                    @NonNull AirshipRuntimeConfig runtimeConfig,
                    @NonNull PreferenceDataStore dataStore) {
        this(inbox, user, channel, dataStore, new MessageCenterResolver(context), new InboxApiClient(runtimeConfig));
    }

    @VisibleForTesting
    InboxJobHandler(@NonNull Inbox inbox,
                    @NonNull User user,
                    @NonNull AirshipChannel channel,
                    @NonNull PreferenceDataStore dataStore,
                    @NonNull MessageCenterResolver resolver,
                    @NonNull InboxApiClient inboxApiClient) {
        this.inbox = inbox;
        this.user = user;
        this.channel = channel;
        this.dataStore = dataStore;
        this.resolver = resolver;
        this.inboxApiClient = inboxApiClient;
    }

    /**
     * Called to handle jobs from {@link Inbox#onPerformJob(UAirship, JobInfo)}.
     *
     * @param jobInfo The airship jobInfo.
     * @return The job result.
     */
    @JobInfo.JobResult
    int performJob(@NonNull JobInfo jobInfo) {
        switch (jobInfo.getAction()) {
            case ACTION_RICH_PUSH_USER_UPDATE:
                onUpdateUser(jobInfo.getExtras().opt(EXTRA_FORCEFULLY).getBoolean(false));
                break;

            case ACTION_RICH_PUSH_MESSAGES_UPDATE:
                onUpdateMessages();
                break;

            case ACTION_SYNC_MESSAGE_STATE:
                onSyncMessages();
                break;
        }

        return JobInfo.JOB_FINISHED;
    }

    /**
     * Updates the message list.
     */
    private void onUpdateMessages() {
        if (!user.isUserCreated()) {
            Logger.debug("InboxJobHandler - User has not been created, canceling messages update");
            inbox.onUpdateMessagesFinished(false);
        } else {
            boolean success = this.updateMessages();
            inbox.refresh(true);
            inbox.onUpdateMessagesFinished(success);
            this.syncReadMessageState();
            this.syncDeletedMessageState();
        }
    }

    /**
     * Sync message sate.
     */
    private void onSyncMessages() {
        this.syncReadMessageState();
        this.syncDeletedMessageState();
    }

    /**
     * Updates the rich push user.
     *
     * @param forcefully If the user should be updated even if its been recently updated.
     */
    private void onUpdateUser(boolean forcefully) {
        if (!forcefully) {
            long lastUpdateTime = dataStore.getLong(LAST_UPDATE_TIME, 0);
            long now = System.currentTimeMillis();
            if (!(lastUpdateTime > now || (lastUpdateTime + USER_UPDATE_INTERVAL_MS) < now)) {
                // Not ready to update
                return;
            }
        }

        boolean success;
        if (!user.isUserCreated()) {
            success = this.createUser();
        } else {
            success = this.updateUser();
        }

        user.onUserUpdated(success);
    }

    /**
     * Update the inbox messages.
     *
     * @return <code>true</code> if messages were updated, otherwise <code>false</code>.
     */
    private boolean updateMessages() {
        Logger.info("Refreshing inbox messages.");

        String channelId = channel.getId();
        if (UAStringUtil.isEmpty(channelId)) {
            Logger.verbose("InboxJobHandler - The channel ID does not exist.");
            return false;
        }

        Logger.verbose("InboxJobHandler - Fetching inbox messages.");

        try {
            Response<JsonList> response = inboxApiClient.fetchMessages(user, channelId, dataStore.getLong(LAST_MESSAGE_REFRESH_TIME, 0));

            Logger.verbose("InboxJobHandler - Fetch inbox messages response: %s", response);

            // 200-299
            if (response.isSuccessful()) {
                JsonList result = response.getResult();
                Logger.info("InboxJobHandler - Received %s inbox messages.", response.getResult().size());
                updateInbox(response.getResult());
                dataStore.put(LAST_MESSAGE_REFRESH_TIME, response.getLastModifiedTime());
                return true;
            }

            // 304
            if (response.getStatus() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                Logger.debug("InboxJobHandler - Inbox messages already up-to-date. ");
                return true;
            }

            Logger.debug("InboxJobHandler - Unable to update inbox messages %s.", response);
            return false;

        } catch (RequestException e) {
            Logger.debug(e, "InboxJobHandler - Update Messages failed.");
            return false;
        }
    }

    /**
     * Update the Rich Push Inbox.
     *
     * @param serverMessages The messages from the server.
     */
    private void updateInbox(JsonList serverMessages) {
        List<JsonValue> messagesToInsert = new ArrayList<>();
        HashSet<String> serverMessageIds = new HashSet<>();

        for (JsonValue message : serverMessages) {
            if (!message.isJsonMap()) {
                Logger.error("InboxJobHandler - Invalid message payload: %s", message);
                continue;
            }

            String messageId = message.optMap().opt(Message.MESSAGE_ID_KEY).getString();
            if (messageId == null) {
                Logger.error("InboxJobHandler - Invalid message payload, missing message ID: %s", message);
                continue;
            }

            serverMessageIds.add(messageId);

            if (resolver.updateMessage(messageId, message) != 1) {
                messagesToInsert.add(message);
            }
        }

        // Bulk insert any new messages
        if (messagesToInsert.size() > 0) {
            resolver.insertMessages(messagesToInsert);
        }

        // Delete any messages that did not come down with the message list
        Set<String> deletedMessageIds = resolver.getMessageIds();
        deletedMessageIds.removeAll(serverMessageIds);
        resolver.deleteMessages(deletedMessageIds);
    }

    /**
     * Synchronizes local deleted message state with the server.
     */
    private void syncDeletedMessageState() {
        Set<String> idsToDelete = resolver.getDeletedMessageIds();

        if (idsToDelete.size() == 0) {
            // nothing to do
            return;
        }

        String channelId = channel.getId();
        if (UAStringUtil.isEmpty(channelId)) {
            Logger.verbose("InboxJobHandler - The channel ID does not exist.");
            return;
        }

        Logger.verbose("InboxJobHandler - Found %s messages to delete.", idsToDelete.size());

        try {
            Response<Void> response = inboxApiClient.syncDeletedMessageState(user, channelId, idsToDelete);
            Logger.verbose("InboxJobHandler - Delete inbox messages response: %s", response);

            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                resolver.deleteMessages(idsToDelete);
            }
        } catch (RequestException e) {
            Logger.debug(e, "InboxJobHandler - Deleted message state synchronize failed.");
            return;
        }
    }

    /**
     * Synchronizes local read messages state with the server.
     */
    private void syncReadMessageState() {
        Set<String> idsToUpdate = resolver.getReadUpdatedMessageIds();

        if (idsToUpdate.size() == 0) {
            // nothing to do
            return;
        }

        String channelId = channel.getId();
        if (UAStringUtil.isEmpty(channelId)) {
            Logger.verbose("InboxJobHandler - The channel ID does not exist.");
            return;
        }

        Logger.verbose("InboxJobHandler - Found %s messages to mark read.", idsToUpdate.size());

        try {
            Response<Void> response = inboxApiClient.syncReadMessageState(user, channelId, idsToUpdate);
            Logger.verbose("InboxJobHandler - Mark inbox messages read response: %s", response);

            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                resolver.markMessagesReadOrigin(idsToUpdate);
            }
        } catch (RequestException e) {
            Logger.debug(e, "InboxJobHandler - Read message state synchronize failed.");
            return;
        }
    }

    /**
     * Create the user.
     *
     * @return <code>true</code> if user was created, otherwise <code>false</code>.
     */
    private boolean createUser() {
        String channelId = channel.getId();
        if (UAStringUtil.isEmpty(channelId)) {
            Logger.debug("InboxJobHandler - No Channel. User will be created after channel registrations finishes.");
            return false;
        }

        try {
            Response<UserCredentials> response = inboxApiClient.createUser(channelId);

            // 200-209
            if (response.isSuccessful()) {
                UserCredentials userCredentials = response.getResult();

                Logger.info("InboxJobHandler - Created Rich Push user: %s", userCredentials.getUsername());
                dataStore.put(LAST_UPDATE_TIME, System.currentTimeMillis());
                dataStore.remove(LAST_MESSAGE_REFRESH_TIME);
                user.onCreated(userCredentials.getUsername(), userCredentials.getPassword(), channelId);
                return true;
            }

            Logger.debug("InboxJobHandler - Rich Push user creation failed: %s", response);
            return false;

        } catch (RequestException e) {
            Logger.debug(e, "InboxJobHandler - User creation failed.");
            return false;
        }
    }

    /**
     * Update the user.
     *
     * @return <code>true</code> if user was updated, otherwise <code>false</code>.
     */
    private boolean updateUser() {
        String channelId = channel.getId();

        if (UAStringUtil.isEmpty(channelId)) {
            Logger.debug("InboxJobHandler - No Channel. Skipping Rich Push user update.");
            return false;
        }

        try {
            Response<Void> response = inboxApiClient.updateUser(user, channelId);
            Logger.verbose("InboxJobHandler - Update Rich Push user response: %s", response);

            if (response.getStatus() == HttpURLConnection.HTTP_OK) {
                Logger.info("Rich Push user updated.");
                dataStore.put(LAST_UPDATE_TIME, System.currentTimeMillis());
                user.onUpdated(channelId);
                return true;
            }

            dataStore.put(LAST_UPDATE_TIME, 0);
            return false;

        } catch (RequestException e) {
            Logger.debug(e, "InboxJobHandler - User update failed.");
            return false;
        }
    }

}
