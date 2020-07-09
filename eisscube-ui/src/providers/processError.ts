export default (error: any, prefix = "eiss") => {
    if (error.response) {
        if (error.response.status === "404") {
            return Promise.reject(new Error(""))
        }
        else if (error.response.status === "500") {
            return Promise.reject(new Error(prefix + ".server_error"));
        } 
        else {
            // Server should return a translatable key for bad requests
            return Promise.reject(new Error(prefix + "." + error.response.data.toLowerCase()));
        }
    }
    // If no `response` property, Error message is assumed to be translatable
    else if (error.message) {
        return Promise.reject(new Error(prefix + '.' + error.message.toLowerCase()));
    }
    // If no response at all
    else {
        return Promise.reject(new Error(prefix + ".no_response"));
    }
};
