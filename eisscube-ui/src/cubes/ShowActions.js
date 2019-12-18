import React, { Fragment } from 'react';
import { EditButton } from 'react-admin';
import { withStyles } from '@material-ui/core/styles';
import CardHeader from '@material-ui/core/CardHeader';
import Avatar from '@material-ui/core/Avatar';
import OnlineIcon from '@material-ui/icons/ThumbUp';
import OfflineIcon from '@material-ui/icons/ThumbDown';
import { red, green } from '@material-ui/core/colors';
import TestButton from './TestButton';
import SetupButton from './SetupButton';
import StatusField from './StatusField';
import StartedAndLastPingField from './StartedAndLastPingField';

const styles = theme => ({
    cardHeader: { 
        zIndex: 2,
        padding: 0,
        marginBottom: theme.spacing.unit 
    },
    onlineAvatar: {
        color: theme.palette.common.white,
        backgroundColor: green[500]
    },
    offlineAvatar: {
        color: theme.palette.common.white,
        backgroundColor: red[500]
    }
});

const ShowActions = withStyles(styles)(
    ({ classes, basePath, data }) => {
        let avatar;
        if (data && data.online) {
            avatar = <Avatar className={classes.onlineAvatar}><OnlineIcon /></Avatar>;
        } else {
            avatar = <Avatar className={classes.offlineAvatar}><OfflineIcon /></Avatar>;
        }
        
        return (
            <CardHeader
                className={classes.cardHeader}
                avatar={avatar}
                title={ <StatusField record={data} /> }
                subheader={ <StartedAndLastPingField record={data} /> }
                action={
                    <Fragment>
                        <TestButton basePath={basePath} record={data} />
                        <SetupButton basePath={basePath} record={data} />
                        <EditButton basePath={basePath} record={data} />
                    </Fragment>
                }
            />
        );
    }
);

export default ShowActions;