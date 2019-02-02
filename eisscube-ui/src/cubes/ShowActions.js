import React from 'react';
import { EditButton } from 'react-admin';
import { withStyles } from '@material-ui/core/styles';
import CardActions from '@material-ui/core/CardActions';
import SetupButton from './SetupButton';

const styles = theme => ({
	cardActions: {
        zIndex: 2,
        display: 'inline-block',
        float: 'right',
        paddingTop: 0,
        paddingRight: 0
    }
});

const ShowActions = withStyles(styles)(
    ({ classes, basePath, data }) => (
        <CardActions className={classes.cardActions} >
            <SetupButton basePath={basePath} record={data} />
            <EditButton basePath={basePath} record={data} />
        </CardActions>
    )
);

export default ShowActions;