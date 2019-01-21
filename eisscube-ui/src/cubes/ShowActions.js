import React from 'react';
import CardActions from '@material-ui/core/CardActions';

import { EditButton } from 'react-admin';
import SetupButton from './SetupButton';
import { CardContent } from '@material-ui/core';

const actionStyle = {
    zIndex: 2,
    display: 'inline-block',
    float: 'right',
    paddingTop: 0,
    paddingRight: 0
};

const ShowActions = ({ basePath, data }) => (
    <CardActions style={actionStyle}>
        <SetupButton basePath={basePath} record={data} />
        <EditButton basePath={basePath} record={data} />
    </CardActions>
);

export default ShowActions;
