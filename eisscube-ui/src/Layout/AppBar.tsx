import { AppBar, UserMenu, MenuItemLink } from 'react-admin';
import { IconButton, Tooltip, Typography } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { Home, Person } from '@material-ui/icons';
import { redirectHome } from '../App';

const useStyles = makeStyles(theme => ({
    title: {
        //color: theme.palette.common.white,
        flex: 1,
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden'
    }
}));

const CustomUserMenu = (props: any) => ( 
    <UserMenu {...props}>
        <MenuItemLink
            to="/profile"
            primaryText="Profile"
            leftIcon={<Person />}
            {...props}
        />
    </UserMenu>
);

const CustomAppBar = (props: any) => {
    const classes = useStyles();

    return (
        <AppBar {...props}  
            classes={classes}
            userMenu={<CustomUserMenu />}
        >
            <Typography className={classes.title} variant="h6" id="react-admin-title" />
            <Tooltip title='Home'>
                <IconButton color="inherit" onClick={redirectHome}>
                    <Home />
                </IconButton>
            </Tooltip>
        </AppBar>
    );
};

export default CustomAppBar;
