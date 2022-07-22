import { stringify } from 'query-string';
import { DataProvider as DataProviderType, fetchUtils } from "ra-core";
import HttpService from "./HttpService";

type Extended = {
	count: (resource: string, params: {filter: any}) => Promise<{data: number}>,
	validate: (resource: string, params: {data: any}) => Promise<{data: any}>,
    usage: (resource: string, params: {data: any}) => Promise<{data: any}>,
    test: (resource: string, params: {data: any}) => Promise<{data: any}>
}

const DataProvider = (apiUrl: string, httpService: HttpService): DataProviderType & Extended => ({

	getList: (resource, params) => {
		let url = '';

		if (!params) {
			url = `${apiUrl}/${resource}`;
		} else {
			const {page, perPage} = params.pagination;
			const {field, order} = params.sort;

			const query = {
				...fetchUtils.flattenObject(params.meta),
				...fetchUtils.flattenObject(params.filter),
				_sort: field,
				_order: order,
				_start: (page - 1) * perPage,
				_end: page * perPage,
			};

			url = `${apiUrl}/${resource}?${stringify(query)}`;
		}

		return httpService.request(url).then(({ headers, json }) => {
			let total = 0;
			if (!headers.has('x-total-count')) {
				throw new Error('Did you declare "X-Total-Count" in the Access-Control-Expose-Headers header?');
			} else {
				total = parseInt(headers.get('x-total-count')!);
			}

			return {
				data: json,
				total: total
			};
		});
	},

	getOne: (resource, params) => {
		const url = `${apiUrl}/${resource}/${params.id}`;

		return httpService.request(url)
		.then(({ json }) => {
			return {data: json};
		});
	},

	getMany: (resource, params) => {
		const query = {
			[`id_like`]: params.ids.join('|'),
		};
		const url = `${apiUrl}/${resource}?${stringify(query)}`;
		return httpService.request(url)
		.then(({ json }) => {
			return {data: json};
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

		return httpService.request(url)
		.then(({ headers, json}) => {
			if (!headers.has('x-total-count')) {
				throw new Error('Did you declare "X-Total-Count" in the Access-Control-Expose-Headers header?');
			}
			return {
				data: json,
				total: parseInt(headers.get('x-total-count')!)
			};
		});
	},

	update: (resource, params) => {
		const url = `${apiUrl}/${resource}/${params.id}`;
		return httpService.request(url, {
			method: 'PUT',
			body: JSON.stringify(params.data)
		})
		.then(({ json }) => {
			return {data: json};
		});
	},

	updateMany: (resource, params) => {
		return Promise.all(
			params.ids.map(id =>
				httpService.request(`${apiUrl}/${resource}/${id}`, {
					method: 'PUT',
					body: JSON.stringify(params.data)
				})
			)
		)
		.then(responses => ({ data: responses.map(({ json }) => json.id) }));
	},

	create: (resource, params) => {
		const url = `${apiUrl}/${resource}`;
		return httpService.request(url, {
			method: 'POST',
			body: JSON.stringify(params.data)
		})
		.then(({ json }) => {
			return {data: { ...params.data, id: json}}
		});
	},

	delete: (resource, params) => {
		const url = `${apiUrl}/${resource}/${params.id}`;
		return httpService.request(url, {
			method: 'DELETE'
		})
		.then(({json}) => {
			return {data: json};
		});
	},

	deleteMany: (resource, params) => {
		return Promise.all(
			params.ids.map(id =>
				httpService.request(`${apiUrl}/${resource}/${id}`, {
					method: 'DELETE'
				})
			)
		)
		.then(responses => ({ data: responses.map(({ json }) => json.id) }))
	},

	count: (resource, params) => {
		const query = {
			...fetchUtils.flattenObject(params.filter),
		};
		const url = `${apiUrl}/${resource}-count?${stringify(query)}`;
		return httpService.request(url)
		.then(({json}) => {
			return {data: json};
		});
	},

	validate: (resource, params) => {
		const url = `${apiUrl}/${resource}/validate`;
		return httpService.request(url, {
			method: 'POST',
			body: JSON.stringify(params.data)
		})
		.then(({json}) => {
			return {data: json};
		})
	},

    usage: (resource, params) => {
        const url = `${apiUrl}/${resource}`;
        return httpService.request(url, {
            method: 'POST',
            body: JSON.stringify(params.data)
        })
        .then(({json}) => {
            return {data: json};
        })
    },

	test: (resource, params) => {
		const url = `${apiUrl}/${resource}/${params.data.id}`;

		return httpService.request(url)
		.then(({ json }) => {
			return {data: json};
		});
	}

});

export default DataProvider;
