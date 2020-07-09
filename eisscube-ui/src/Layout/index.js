import React from 'react';
import CubeAppBar from './AppBar';
import { Layout } from 'react-admin';

const CubeLayout = (props) => (
    <Layout {...props} 
        appBar={CubeAppBar}
    />
);

export default CubeLayout;