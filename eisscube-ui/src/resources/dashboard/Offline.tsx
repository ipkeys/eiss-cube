import { makeStyles, Theme } from '@material-ui/core/styles';
import { Avatar, Card, CardHeader } from '@material-ui/core';
import { ThumbDown, List } from '@material-ui/icons';
import { red } from '@material-ui/core/colors';
import { Button } from 'react-admin';

const useStyles = makeStyles((theme: Theme) => ({ 
    card: { 
        borderLeft: `solid 4px ${red[500]}`, 
        flex: 1, 
        marginBottom: theme.spacing(1) 
    },
    avatar: {
        color: theme.palette.common.white,
        backgroundColor: red[500]
    }
}));

const Offline = (props: any) => {
    const { value } = props;
    const classes = useStyles();

    return (
        <Card className={classes.card} >
            <CardHeader
                avatar={
                    <Avatar className={classes.avatar}>
                        <ThumbDown />
                    </Avatar>
                }
                title={`${value} EISS™Cube(s)`}
                subheader={<span style={{ color: red[500] }}>OFFLINE</span>}
                action={
                    <Button label='List'
                        href={'#/cubes?filter={"online":false}&page=1&perPage=10&sort=deviceID&order=DESC'} >
                        <List />
                    </Button>
                }
            />
        </Card>
    );
}

export default Offline;
