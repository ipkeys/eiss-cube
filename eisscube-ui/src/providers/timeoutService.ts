import TokenManager from './tokenManager';

// The amount of time between checks
const CHECK_DURATION = 15; // seconds

// Track the duration of no activity
let _idleCounter = 0;

document.addEventListener("click", () => {
    _idleCounter = 0;
});

document.addEventListener("mousemove", () => {
    _idleCounter = 0;
});

document.addEventListener("keypress", () => {
    _idleCounter = 0;
});

/**
 * This checks for user activity and logs the user out when inactive
 */
export default class ActivityTimeout {
    private interval: number;
    private logout: () => void;
    private tokenManager: TokenManager;

    constructor(tokenManager: TokenManager, logout: () => void, loggedin: boolean) {
        this.tokenManager = tokenManager;
        this.logout = logout;

        if (loggedin) {
            this.start();
        }
    }

    start = () => {
        this.interval = window.setInterval(this.checkIdleTime, CHECK_DURATION * 1000);
    };

    cancel = () => {
        clearInterval(this.interval);
    };

    checkIdleTime = () => {
        _idleCounter++;
        const timeoutDuration = this.tokenManager.getTimeoutDuration();
        if (timeoutDuration === null || _idleCounter * CHECK_DURATION >= Number(timeoutDuration) * 60) {
            this.logout();
        }
    };
}
