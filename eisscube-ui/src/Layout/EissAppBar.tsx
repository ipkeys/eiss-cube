import { forwardRef } from 'react';
import { AppBar, Logout, UserMenu, useUserMenu } from 'react-admin';
import { Link } from 'react-router-dom';
import {
	MenuItem,
	ListItemIcon,
	ListItemText,
	IconButton,
	Tooltip,
	Typography
} from '@mui/material';
import { Home, Person } from '@mui/icons-material';
import { development } from '../global';

const ProfileMenu = forwardRef((props, _ref) => {
	const { onClose } = useUserMenu();

	return (
		<MenuItem
			component={Link}
			// @ts-ignore
			//ref={ref}
			to="/profile"
			onClick={onClose}
			{...props}
		>
			<ListItemIcon>
				<Person />
			</ListItemIcon>
			<ListItemText>
				Profile
			</ListItemText>
		</MenuItem>
	);
});

const EissUserMenu = () => (
    <UserMenu>
        <ProfileMenu />
        <Logout />
    </UserMenu>
);

const EissAppBar = () => (
	<AppBar userMenu={<EissUserMenu />} >
		<Typography id='react-admin-title' variant='h6' sx={{
			color: theme => theme.palette.common.white,
			flex: 1,
			textOverflow: 'ellipsis',
			whiteSpace: 'nowrap',
			overflow: 'hidden'
		}}/>

		<Tooltip title='Home'>
			<IconButton sx={{ padding: '12px', color: theme => theme.palette.common.white }}
				onClick={() => {
					sessionStorage.removeItem("application");
					window.location.href = development ? '#/' : "/ui/home/";
				}}
			>
				<Home />
			</IconButton>
		</Tooltip>
	</AppBar>
);

export default EissAppBar;
