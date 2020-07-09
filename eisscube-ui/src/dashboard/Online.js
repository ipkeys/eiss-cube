import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import Avatar from '@material-ui/core/Avatar';
import OnlineIcon from '@material-ui/icons/ThumbUp';
import ListIcon from '@material-ui/icons/List';
import { green } from '@material-ui/core/colors';

import { Button } from 'react-admin';

const styles = theme => ({
    card: { 
        borderLeft: `solid 4px ${green[500]}`, 
        flex: 1, 
        marginBottom: theme.spacing(1) 
    },
    avatar: {
        color: theme.palette.common.white,
        backgroundColor: green[500]
    }
});

export default withStyles(styles)(
    ({classes, value}) => (
        <Card className={classes.card} >
            <CardHeader
                avatar={
                    <Avatar className={classes.avatar}>
                        <OnlineIcon />
                    </Avatar>
                }
                title={`${value} EISS™Cube(s)`}
                subheader={<span style={{ color: green[500] }}>ONLINE</span>}
                action={
                    <Button
                        label='List' 
                        href={'#/cubes?filter={"online":true}&page=1&perPage=10&sort=deviceID&order=DESC'} >
                        <ListIcon />
                    </Button>
                }
            />
        </Card>
    )
);
