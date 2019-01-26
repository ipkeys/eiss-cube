import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Card from '@material-ui/core/Card';
import CardHeader from '@material-ui/core/CardHeader';
import CardContent from '@material-ui/core/CardContent';
import Typography from '@material-ui/core/Typography';
import Avatar from '@material-ui/core/Avatar';
import LightBulbIcon from '@material-ui/icons/LightbulbOutline';
import { common, blue } from '@material-ui/core/colors';

const styles = theme => ({
    card: { 
        borderLeft: `solid 4px ${blue[500]}`, 
        flex: 1, 
        marginBottom: theme.spacing.unit 
    },
    avatar: {
        color: common.white,
        backgroundColor: blue[500]
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
            <CardContent>
                <Typography>
                    Take a look around...
                </Typography>
            </CardContent>
        </Card>
    )
);
