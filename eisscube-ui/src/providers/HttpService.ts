import { fetchUtils, Options } from 'react-admin';
import TokenManager from './TokenManager';

//----------------------------------------------------------------------------
// HTTP methods which insert Authorization header with JWT.
// All errors are processed and checked for authorization issues.
//----------------------------------------------------------------------------
export default class HttpService {

	constructor(private tokenManager: TokenManager) {}

	public request = (url: string, options: Options = {}) => {
		return this.tokenManager.getToken()
		.then((token: any) => {
			if (token) {
				options.user = {
					authenticated: true,
					token: `Bearer ${token}`
				};
			}

			return fetchUtils.fetchJson(url, options);
		});
	}

}
