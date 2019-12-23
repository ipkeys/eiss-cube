import React from 'react';
import CustomAppBar from './AppBar';
import { Layout } from 'react-admin';

const CustomLayout = (props) => (
    <Layout {...props} 
        appBar={CustomAppBar}
    />
);

export default CustomLayout;