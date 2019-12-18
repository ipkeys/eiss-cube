import React from 'react';
import { Layout, Sidebar } from 'react-admin';
import AppBar from './AppBar';

const EissCubeSidebar = props => 
    <Sidebar size={200} {...props} />;

const EissCubeLayout = props => 
    <Layout appBar={AppBar} sidebar={EissCubeSidebar} {...props} />;

export default EissCubeLayout;
