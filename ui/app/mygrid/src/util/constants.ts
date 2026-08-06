
export class Constants {

    // Attribute names
    static CHALLENGE_DURATION_ATTRIBUTE: string = 'challengeDuration';
    static CHALLENGE_WAIT_ATTRIBUTE: string = 'challengeWait';
    static CHALLENGE_POINT_CURRENT_ATTRIBUTE: string = 'challengePointsCurrent';
    static CHALLENGE_POINT_INTERVAL_ATTRIBUTE: string = 'challengeEarnPointInterval';
    static CHALLENGE_POINT_MAX_CURRENTLY_ATTRIBUTE: string = 'challengePointsTotal';
    static CHALLENGE_POINTS_ATTRIBUTE: string = 'challengePoints';
    static CHALLENGE_START_TIME_ATTRIBUTE: string = 'challengeStart';
    static CHALLENGE_JOINED_ATTRIBUTE: string = 'challengesJoined';
    static CHALLENGE_END_TIME_ATTRIBUTE: string = 'challengeEnd';

    static METER_POWER_ATTRIBUTE: string = 'power';
    static METER_SOLAR_POWER_ATTRIBUTE: string = 'pvpower';
    static METER_POWER_MAX_ATTRIBUTE: string = 'powerMax';
    static METER_PEAK_POINTS_ATTRIBUTE: string = 'peakPoints';
    static METER_PEAK_DAY_POINTS_ATTRIBUTE: string = 'peakPointsDay';

    // Local storage related
    static LOCALSTORAGE_LAST_CHALLENGE_COMPLETED_KEY: string = 'lastChallengeCompleted';

    // URL parameters related
    static CHALLENGE_NOTIFICATION_PARAMS_NAME: string = 'challengeNotification'

    // EARN-E / ENODE related
    static AUTHORIZE_EV_URL: string = "https://earne.welvaart-it.com/public/ourgrid/linkuser/ev/{meterId}/ourgridXmEmF76TSf0j4dsymsDeKDukB9f393hCpP1Cz4v7YpH2pdYjDwrf6ePg7"
}
