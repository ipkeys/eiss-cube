import React from 'react';
import {AppBar, MenuItemLink} from 'react-admin';
import {Home} from '@material-ui/icons';
import {Typography, IconButton, Tooltip} from '@material-ui/core';
import {withStyles} from '@material-ui/core/styles';
import {Person} from '@material-ui/icons';
import {redirectHome} from '../globalExports';
import UserMenu from './UserMenu';

const appBarStyles = (theme => ({
    title: {
        flex: 1,
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
        marginLeft: '32px',
    },
}));

const CustomUserMenu = props => {
    return (
        <UserMenu {...props}>
            <MenuItemLink 
                to={'/profile'}
                primaryText="Profile" 
                leftIcon={<Person />}
                onClick={props.onMenuClick} />
        </UserMenu>
    );
}

const CustomAppBar = ({classes, ...props}) => {
    return (
        <AppBar
            userMenu={<CustomUserMenu />}
            classes={classes}
        {...props} >
            <Typography
                variant="title"
                color="inherit"
                className={classes.title}
                id="react-admin-title"
            />
            <Tooltip title="Home">
                <IconButton
                    color="inherit"
                    onClick={redirectHome}
                >
                    <Home />
                </IconButton>
            </Tooltip>
        </AppBar>
    );
};

export default withStyles(appBarStyles)(CustomAppBar);

