import React from 'react';
import {AppBar, MenuItemLink} from 'react-admin';
import {Home} from '@material-ui/icons';
import {IconButton, Tooltip} from '@material-ui/core';
import {withStyles} from '@material-ui/core/styles';
import {Person} from '@material-ui/icons';
import {redirectHome} from '../App';
import UserMenu from './UserMenu';

const styles = theme => ({
    title: {
        flex: 1,
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden'
    }
});

const CubeUserMenu = props => {
    return (
        <UserMenu {...props}>
            <MenuItemLink 
                to={'/profile'}
                primaryText='Profile' 
                leftIcon={<Person />}
                onClick={props.onMenuClick} />
        </UserMenu>
    );
}

const CubeAppBar = ({classes, ...props}) => {
    return (
        <AppBar classes={classes}
            userMenu={<CubeUserMenu />}
            {...props} 
        >
            <div id='react-admin-title' className={classes.title} />
            <Tooltip title='Home'>
                <IconButton color="inherit" onClick={redirectHome}>
                    <Home />
                </IconButton>
            </Tooltip>
        </AppBar>
    );
};

export default withStyles(styles)(CubeAppBar);

