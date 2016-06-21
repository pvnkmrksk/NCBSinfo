package com.rohitsuratekar.NCBSinfo.common;

public interface UserInformation {

    String MODE = "app_mode";
    String ONLINE = "online";
    String OFFLINE = "offline";

    interface registration{
        String REGISTERED = "registeredUser";  //Boolen
        String USERNAME = "currentUsername"; //String
        String EMAIL = "currentEmail"; //String
        String RESEARCH_TALK = "currentResearchTalk"; //Int
        String FIREBASE_TOKEN = "firebaseRegistrationToken";
        String USER_TYPE = "userType";

        interface camp16 {
            String IS_CAMP_USER = "isCampUser";
            String CAMP_PATTERN = "camp.ncbs.res.in";
            String CAMP_MODE = "camp_mode";
        }

    }

    interface preferences{

        String NOTIFICATIONS = "notification_preference";

    }

    interface netwrok{
        String REGISTRATION_DETAILS_SENT = "registrationDetails";
        String LAST_REFRESH_REMOTE_CONFIG = "lastRefreshRemoteConfig";
        String LAST_DATA_FETCH = "lastDataFetch";
        String IS_OLD_VERSION = "isOldVersion";
    }

    interface firstTime{
        String APP_OPEN = "firstTimeAppOpen";
        String CAMP_NOTICE = "camp_notice_firstTime";
    }



}