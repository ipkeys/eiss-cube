import { makeStyles, Theme } from '@material-ui/core/styles';
import { Avatar, Card, CardHeader } from '@material-ui/core';
import { ThumbUp, List } from '@material-ui/icons';
import { green } from '@material-ui/core/colors';
import { Button } from 'react-admin';

const useStyles = makeStyles((theme: Theme) => ({ 
    card: { 
        borderLeft: `solid 4px ${green[500]}`, 
        flex: 1, 
        marginBottom: theme.spacing(1) 
    },
    avatar: {
        color: theme.palette.common.white,
        backgroundColor: green[500]
    }
}));

const Online = (props: any) => {
    const { value } = props; 
    const classes = useStyles();

    return (
        <Card className={classes.card} >
            <CardHeader
                avatar={
                    <Avatar className={classes.avatar}>
                        <ThumbUp />
                    </Avatar>
                }
                title={`${value} EISS™Cube(s)`}
                subheader={<span style={{ color: green[500] }}>ONLINE</span>}
                action={
                    <Button label='List' 
                        href={'#/cubes?filter={"online":true}&page=1&perPage=10&sort=deviceID&order=DESC'} >
                        <List />
                    </Button>
                }
            />
        </Card>
    );
}

export default Online;
