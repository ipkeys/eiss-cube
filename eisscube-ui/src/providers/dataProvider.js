import { stringify } from 'query-string';
import {
    fetchUtils,
    GET_LIST,
    GET_ONE,
    GET_MANY,
    GET_MANY_REFERENCE,
    CREATE,
    UPDATE,
    UPDATE_MANY,
    DELETE,
    DELETE_MANY,
} from 'react-admin';

export const VALIDATE = "VALIDATE";

let http;

/**
 * Maps react-admin queries to a json-server powered REST API
 *
 * @see https://github.com/typicode/json-server
 * @example
 * GET_LIST     => GET http://my.api.url/posts?_sort=title&_order=ASC&_start=0&_end=24
 * GET_ONE      => GET http://my.api.url/posts/123
 * GET_MANY     => GET http://my.api.url/posts/123, GET http://my.api.url/posts/456, GET http://my.api.url/posts/789
 * UPDATE       => PUT http://my.api.url/posts/123
 * CREATE       => POST http://my.api.url/posts/123
 * DELETE       => DELETE http://my.api.url/posts/123
 */
export default (apiUrl, httpService) => {
    /**
     * @param {String} type One of the constants appearing at the top if this file, e.g. 'UPDATE'
     * @param {String} resource Name of the resource to fetch, e.g. 'posts'
     * @param {Object} params The data request params, depending on the type
     * @returns {Object} { url, options } The HTTP request parameters
     */

    http = httpService;

    const convertDataRequestToHTTP = (type, resource, params) => {
        let url = '';
        const options = {};

        switch (type) {
            case GET_LIST: {
                if (!params) {
                    url = `${apiUrl}/${resource}`;
                    break;
                }
                const { page, perPage } = params.pagination;
                const { field, order } = params.sort;
                const query = {
                    ...fetchUtils.flattenObject(params.filter),
                    _sort: field,
                    _order: order,
                    _start: (page - 1) * perPage,
                    _end: page * perPage,
                };
                url = `${apiUrl}/${resource}?${stringify(query)}`;
                break;
            }
            case GET_ONE:
                url = `${apiUrl}/${resource}/${params.id}`;
                break;
            case GET_MANY_REFERENCE: {
                const { page, perPage } = params.pagination;
                const { field, order } = params.sort;
                const query = {
                    ...fetchUtils.flattenObject(params.filter),
                    [params.target]: params.id,
                    _sort: field,
                    _order: order,
                    _start: (page - 1) * perPage,
                    _end: page * perPage,
                };
                url = `${apiUrl}/${resource}?${stringify(query)}`;
                break;
            }
            case UPDATE:
                url = `${apiUrl}/${resource.replace('/update/', '')}/${params.id}`;
                options.method = 'put';
                // options.body = {data: params.data, previousData: params.previousData};
                options.body = params.data;
                break;
            case CREATE:
                url = `${apiUrl}/${resource.replace('/create/', '')}`;
                options.method = 'post';
                options.body = params.data;
                break;
            case DELETE:
                url = `${apiUrl}/${resource}/${params.id}`;
                options.method = 'delete';
                break;
            case GET_MANY: {
                const query = {
                    [`id_like`]: params.ids.join('|'),
                };
                url = `${apiUrl}/${resource}?${stringify(query)}`;
                break;
            }
            case VALIDATE: {
                url = `${apiUrl}/${resource}/validate`;
                options.body = params.data;
                options.method = 'post';
                break;
            }
            default:
                throw new Error(`Unsupported fetch action type ${type}`);
        }
        // console.log(type, url, options);
        return { url, options };
    };

    /**
     * @param {Object} response HTTP response from fetch()
     * @param {String} type One of the constants appearing at the top if this file, e.g. 'UPDATE'
     * @param {String} resource Name of the resource to fetch, e.g. 'posts'
     * @param {Object} params The data request params, depending on the type
     * @returns {Object} Data response
     */
    const convertHTTPResponse = (response, type, resource, params) => {
        const { headers, data } = response;
        // console.log(resource, response, params);
        switch (type) {
            case GET_MANY:
                return { data: data };
            case GET_LIST:
            case GET_MANY_REFERENCE:
                if (!headers['x-total-count']) {
                    throw new Error('Did you declare "x-total-count" in the Access-Control-Expose-Headers header?');
                }
                return {
                    data: data,
                    total: parseInt(headers['x-total-count'], 10)
                };
            case CREATE:
                return { data: { ...params.data, id: data.id } };
            case VALIDATE:
                return response;
            default:
                return { data: data };
        }
    };

    /**
     * @param {string} type Request type, e.g GET_LIST
     * @param {string} resource Resource name, e.g. "posts"
     * @param {Object} payload Request parameters. Depends on the request type
     * @returns {Promise} the Promise for a data response
     */
    return (type, resource, params) => {
        // json-server doesn't handle filters on UPDATE route, so we fallback to calling UPDATE n times instead
        if (type === UPDATE_MANY) {
            return Promise.all(
                params.ids.map(id =>
                    httpClient(`${apiUrl}/${resource}/${id}`, {
                        method: 'put',
                        body: JSON.stringify(params.data),
                    })
                )
            ).then(responses => ({
                data: responses.map(response => response.json),
            }));
        }
        // json-server doesn't handle filters on DELETE route, so we fallback to calling DELETE n times instead
        if (type === DELETE_MANY) {
            return Promise.all(
                params.ids.map(id =>
                    httpClient(`${apiUrl}/${resource}/${id}`, {
                        method: 'delete',
                    })
                )
            ).then(responses => ({
                data: responses.map(response => response.json),
            }));
        }

        const { url, options } = convertDataRequestToHTTP(type, resource, params);
        return httpClient(url, options)
        .then(
            (response) => {
                return convertHTTPResponse(response, type, resource, params)
            },
            (error) => {
                // Pass validation error handling upstream
                if (type === VALIDATE) {
                    return new Error(error.response);
                }  
                
                // Axios error
                if (error.message) {
                    console.log(error.message);
                    return new Error("axios.error");
                }

                return http.processError(error);
            }
        );
    };
};

const httpClient = (url, options = {}) => {
    const method = options.method || 'get'; 
    return http[method](url, options);
};
  