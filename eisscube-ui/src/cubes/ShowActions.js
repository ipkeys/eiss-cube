import React from 'react';
import { TopToolbar, EditButton } from 'react-admin';
import { withStyles } from '@material-ui/core/styles';
import CardHeader from '@material-ui/core/CardHeader';
import Avatar from '@material-ui/core/Avatar';
import OnlineIcon from '@material-ui/icons/ThumbUp';
import OfflineIcon from '@material-ui/icons/ThumbDown';
import { red, green } from '@material-ui/core/colors';
//import RebootButton from './RebootButton';
import TestButton from './TestButton';
import SetupButton from './SetupButton';
import StatusField from './StatusField';
import StartedAndLastPingField from './StartedAndLastPingField';

const styles = theme => ({
    cardHeader: { 
        position: 'absolute',
        left: 0,
        padding: 0
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

const ShowActions = ({ classes, basePath, data }) => {
        let avatar;
        if (data && data.online) {
            avatar = <Avatar className={classes.onlineAvatar}><OnlineIcon /></Avatar>;
        } else {
            avatar = <Avatar className={classes.offlineAvatar}><OfflineIcon /></Avatar>;
        }
        
        return (
            <TopToolbar >
                <CardHeader className={classes.cardHeader}
                    avatar={avatar}
                    title={ <StatusField record={data} /> }
                    subheader={ <StartedAndLastPingField record={data} /> }
                />
                
                {/* <RebootButton basePath={basePath} record={data} /> disable until new firmware */}
                
                <TestButton basePath={basePath} record={data} />
                <SetupButton basePath={basePath} record={data} />
                <EditButton basePath={basePath} record={data} />
            </TopToolbar>
        );
    };

export default withStyles(styles)(ShowActions);