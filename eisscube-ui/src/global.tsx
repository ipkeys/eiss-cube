/**
 * This file is for handling enviornment variables exposed throughout the app, 
 * and allows for easy changing of these global values if needed
 */

const development = (process.env.NODE_ENV === "development");

const authUrl = development ? `http://localhost:${process.env.REACT_APP_AUTH_PORT}/auth` : "/auth";
const apiUrl  = development ? `http://localhost:${process.env.REACT_APP_CUBE_PORT}` : "/cube";

export {
    development,
    authUrl,
    apiUrl
};
 