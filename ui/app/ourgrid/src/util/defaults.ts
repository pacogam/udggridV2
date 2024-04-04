
export class Defaults {

    // Challenge related
    static CHALLENGE_DURATION_MINUTES = 60;
    static CHALLENGE_INTERVAL_MINUTRES = 6;
    static CHALLENGE_WAIT_MINUTES = 15;

    // Challenge datapoint related
    static CHALLENGE_PROGRESS_DATAPOINT_INTERVAL_SECONDS = 30;
    static CHALLENGE_PROGRESS_DIGIT_AMOUNT = 3;

    // Challenge history related
    static HISTORY_FETCH_AMOUNT: number = 1;
    static HISTORY_FETCH_UNIT: string = 'week';

    // Point earnings related
    static POINT_EXCHANGE_RATE_DECIMALS = 3;
    static POINT_EARNINGS_DECIMALS = 2;

    // Tips related
    static TIPS_HEAT_PUMP_WATT_SAVED: number = 3000;
    static TIPS_VEHICLE_CHARGER_WATT_SAVED: number = 5000;

    // Animation related
    static PEAK_NOTIFICATION_JOINED_TIMEOUT_MS = 3000;
    static HOUSEHOLD_CHARACTERISTICS_SAVE_BUTTON_TIMEOUT_MS = 5000;

    // Styling related
    static HIDE_HEADER_FROM_HEIGHT_PX: number = 240;
}
