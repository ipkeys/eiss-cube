import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import Avatar from '@material-ui/core/Avatar';
import LightBulbIcon from '@material-ui/icons/HighlightOutlined';

const styles = theme => ({
    card: { 
        borderLeft: `solid 4px ${theme.palette.primary.main}`, 
        flex: 1, 
        marginBottom: theme.spacing.unit 
    },
    avatar: {
        color: theme.palette.common.white,
        backgroundColor: theme.palette.primary.main
    }
});

export default withStyles(styles)(
    ({classes}) => (
        <Card className={classes.card} >
            <CardHeader
                avatar={
                    <Avatar className={classes.avatar}>
                        <LightBulbIcon />
                    </Avatar>
                }
                title='Welcome to the EISS™Cube Server'
                subheader='Handling EISS™Cubes, sending commands, collecting reports...'
            />
        </Card>
    )
);
