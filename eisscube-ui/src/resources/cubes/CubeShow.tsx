import { makeStyles, Theme } from '@material-ui/core/styles';
import { 
    Show,
    TopToolbar,
    ListButton,
    EditButton,
    useRecordContext 
} from 'react-admin';
import {
    Avatar,
    CardHeader
 } from '@material-ui/core';
 import ChevronLeft from '@material-ui/icons/ChevronLeft';
 import OnlineIcon from '@material-ui/icons/ThumbUp';
import OfflineIcon from '@material-ui/icons/ThumbDown';
import { red, green } from '@material-ui/core/colors';

import { NavTitle } from '../common';

import CubeMap from './CubeMap';
import RebootButton from './buttons/RebootButton';
import TestButton from './buttons/TestButton';
import SetupButton from './buttons/SetupButton';
import StatusField from './fields/StatusField';
import StartedAndLastPingField from './fields/StartedAndLastPingField';

const useStyles = makeStyles((theme: Theme) => ({ 
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
}));

const ShowToolbar = ({basePath, ...props}: any) => {
    const classes = useStyles();
    const record = useRecordContext(props);

    let avatar;
    if (record && record.online) {
        avatar = <Avatar className={classes.onlineAvatar}><OnlineIcon /></Avatar>;
    } else {
        avatar = <Avatar className={classes.offlineAvatar}><OfflineIcon /></Avatar>;
    }
    
    return (
        <TopToolbar >
            <CardHeader className={classes.cardHeader}
                avatar={avatar}
                title={<StatusField {...props} />}
                subheader={<StartedAndLastPingField {...props}/>}
            />
            <ListButton basePath={basePath} label="Back" icon={<ChevronLeft />} />
            <RebootButton {...props} />              
            <TestButton {...props} />
            <SetupButton {...props} />
            <EditButton  basePath={basePath} record={record} />
        </TopToolbar>
    );
};

const CubeShow = (props: any) => {
    return (
        <Show
            title={<NavTitle title='Manage EISS™Cube' />}
            actions={<ShowToolbar />}
            {...props}
        >
            <CubeMap {...props}/>
        </Show>
    )
};

export default CubeShow;
