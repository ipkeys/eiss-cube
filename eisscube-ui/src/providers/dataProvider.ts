import { AxiosResponse } from 'axios';
import { stringify } from 'query-string';
import { DataProvider as DataProviderType, fetchUtils, HttpError } from "ra-core";
import HttpService from "./HttpService";
import { prefix } from './i18nProvider';

type Extended = {
    count: (resource: string, params: {filter: any}) => Promise<{data: number}>,
    validate: (resource: string, params: {data: any}) => Promise<AxiosResponse<any>>
    usage: (resource: string, params: {data: any}) => Promise<AxiosResponse<any>>
}

const DataProvider = (apiUrl: string, http: HttpService): DataProviderType & Extended => ({
    
    getList: (resource, params) => {
        let url = '';

        if (!params) {
            url = `${apiUrl}/${resource}`;
        }
        else {
            const {page, perPage} = params.pagination;
            const {field, order} = params.sort;

            const query = {
                ...fetchUtils.flattenObject(params.filter),
                _sort: field,
                _order: order,
                _start: (page - 1) * perPage,
                _end: page * perPage,
            };

            url = `${apiUrl}/${resource}?${stringify(query)}`;
        }

        return new Promise(async (resolve, reject) => {
            http.get(url)
            .then(({data, headers}) => {
                if (!headers['x-total-count']) {
                    throw new Error('Did you declare "X-Total-Count" in the Access-Control-Expose-Headers header?');
                }
    
                return resolve({
                    data: data.map(formatRecord),
                    total: parseInt(headers["x-total-count"]) 
                });
            })
            .catch((error) => {
                return reject(processError(error));
            });
        });
        
    },

    getOne: (resource, params) => {
        const url = `${apiUrl}/${resource}/${params.id}`;

        return new Promise(async (resolve, reject) => {
            http.get(url)
            .then(({data}) => {
                return resolve({data: formatRecord(data)});
            })
            .catch((error) => {
                return reject(processError(error));
            });
        });
    },

    getMany: (resource, params) => {
        const query = {
            [`id_like`]: params.ids.join('|'),
        };
        const url = `${apiUrl}/${resource}?${stringify(query)}`;
        return new Promise(async (resolve, reject) => {
            http.get(url)
            .then(({data}) => {
                return resolve({
                    data: data.map(formatRecord)
                });
            })
            .catch((error) => {
                return reject(processError(error));
            });
        });
    },

    getManyReference: (resource, params) => {
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
        const url = `${apiUrl}/${resource}?${stringify(query)}`;

        return new Promise(async (resolve, reject) => {
            http.get(url)
            .then(({data, headers}) => {
                if (!headers['x-total-count']) {
                    throw new Error('Did you declare "X-Total-Count" in the Access-Control-Expose-Headers header?');
                }
                return resolve({
                    data: data.map(formatRecord),
                    total: parseInt(headers["x-total-count"]) 
                });
            })
            .catch((error) => {
                return reject(processError(error));
            });
        });
    },

    update: (resource, params) => {
        const url = `${apiUrl}/${resource}/${params.id}`;
        return new Promise(async (resolve, reject) => {
            http.put(url, {body: JSON.stringify(params.data)})
            .then(({data}) => {
                return resolve({data: formatRecord(data)});
            })
            .catch((error) => {
                return reject(processError(error));
            });
        });
    },

    updateMany: (resource, params) => {
        return Promise.all(
            params.ids.map(id => 
                http.put(`${apiUrl}/${resource}/${id}`, {body: JSON.stringify(params.data)})
            )
        )
        .then(responses => ({
            data: responses.map(data => formatRecord(data).id)
        }))
        .catch(error => {
            return Promise.reject(processError(error));
        })
    },

    create: (resource, params) => {
        const url = `${apiUrl}/${resource}`;
        return new Promise(async (resolve, reject) => {
            http.post(url, {body: JSON.stringify(params.data)})
            .then(({data}) => {
                return resolve({data: { ...params.data, id: formatRecord(data.id)}})
            })
            .catch((error) => {
                return reject(processError(error));
            })
        });
    },

    delete: (resource, params) => {
        const url = `${apiUrl}/${resource}/${params.id}`;
        return new Promise(async (resolve, reject) => {
            http.delete(url)
            .then(({data}) => {
                return resolve({data: formatRecord(data)});
            })
            .catch((error) => {
                return reject(processError(error));
            });
        });
    },

    deleteMany: (resource, params) => {
        return Promise.all(
            params.ids.map(id => 
                http.delete(`${apiUrl}/${resource}/${id}`)
            )
        )
        .then(responses => ({
            data: responses.map(data => formatRecord(data).id)
        }))
        .catch((error) => {
            return Promise.reject(processError(error));
        })
    },

    count: (resource, params) => {
        const query = {
            ...fetchUtils.flattenObject(params.filter),
        };
        const url = `${apiUrl}/${resource}-count?${stringify(query)}`;
        return new Promise(async (resolve, reject) => {
            http.get(url)
            .then(({data}) => {
                return resolve({data});
            })
            .catch((error) => {
                return reject(processError(error));
            })
        })
    },
    
    validate: (resource, params) => {
        const url = `${apiUrl}/${resource}/validate`; 
        return new Promise(async (resolve, reject) => {
            http.post(url, {body: params.data})
            .then((response) => {
                return resolve(response);
            })
            .catch(error => {
                // Pass validation error handling upstream
                return reject(error);
            })
        })
    },

    usage: (resource, params) => {
        const url = `${apiUrl}/${resource}`;
        return new Promise(async (resolve, reject) => {
            http.post(url, {body: params.data})
            .then((response) => {
                return resolve(response);
            })
            .catch(error => {
                return reject(error);
            })
        })
    }
});

function formatRecord (record: any) {
    if (record["_id"]) record.id = record["_id"];
    return record;
} 

function processError(error: any) {
    if (error.message === "auth.expired" || error.message.statusText === "token expired") {
        return new HttpError(`${prefix}.auth.expired`, 401);
    }
    else if (error.message === 'no_response') {
        return new HttpError(`${prefix}.no_response`, 504);
    }
    else if (error.message === "Network Error") {
        return new HttpError("axios.error", 504);
    }
    else if (error.response) {
        if (error.response.status === 404) {
            return new HttpError("ra.page.not_found", 404);
        }
        else {
            return new HttpError(`${prefix}.server_error`, error.response.status);
        }
    }
    else {
        return new HttpError(`${prefix}.server_error`, 500);
    }
}

export default DataProvider;
