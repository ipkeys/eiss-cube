import React from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';
import createCache from '@emotion/cache';
import { CacheProvider } from "@emotion/react";

export const muiCache = createCache({
	'key': 'mui',
	'prepend': true,
});

const container = document.getElementById('root');
const root = createRoot(container);

root.render(
	<React.StrictMode>
		<CacheProvider value={muiCache}>
			<App />
		</CacheProvider>
	</React.StrictMode>
);
