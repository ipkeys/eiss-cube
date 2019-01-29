import React from 'react';
import { AppBar, UserMenu, MenuItemLink } from 'react-admin';
import SettingsIcon from '@material-ui/icons/Settings';

const EissCubeUserMenu = ({ ...props }) => (
    <UserMenu {...props}>
        <MenuItemLink
            to="/change_password"
            primaryText="Change Username/Password"
            leftIcon={<SettingsIcon />}
        />
    </UserMenu>
);

const EissCubeAppBar = props => (
    <AppBar {...props} userMenu={<EissCubeUserMenu />} />
);

export default EissCubeAppBar;
