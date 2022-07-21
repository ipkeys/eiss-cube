import { useState } from 'react';
import {
	Button,
	useDataProvider,
	useRecordContext,
	useNotify
} from 'react-admin';
import {
	Box,
	Typography,
	Dialog,
	DialogTitle,
	DialogContent,
	DialogActions,
	alpha
} from '@mui/material';
import { red } from '@mui/material/colors';
import RebootIcon from '@mui/icons-material/Autorenew';

const RebootButton = (props: any) => {
	const [showDialog, setShowDialog] = useState<boolean>(false);
	const notify = useNotify();
	const record = useRecordContext(props);
	const dataProvider = useDataProvider();

	const handleOpen = () => {
		setShowDialog(true);
	};

	const handleClose = () => {
		setShowDialog(false);
	};

	const handleClick = () => {
		setShowDialog(false);

		dataProvider.create('commands', {
			data: { cubeID: record.id, command: 'reboot', deviceType: 'e' }
		})
		.then(() => {
			notify('eiss.reboot', {type: 'warning'});
		})
		.catch((e) => {
			notify(`Error: ${e}`, {type: 'warning'})
		});
	};

	return (
		record &&
		<Box>
			<Button label='Reboot...'
				disabled={record.online === false}
				onClick={handleOpen}
				sx={{
					color: red[500],
					'&:hover': {
						backgroundColor: alpha(red[50], 0.5),
						'@media (hover: none)': {
							backgroundColor: 'transparent',
						},
					}
				}}
			>
				<RebootIcon />
			</Button>
			<Dialog
				open={showDialog}
				keepMounted
				onClose={handleClose}
				aria-labelledby='reboot-dialog-title'
				aria-describedby='reboot-dialog-description'
			>
				<DialogTitle id='reboot-dialog-title'>
					<Typography sx={{ display: 'inline-flex' }}>
						<RebootIcon sx={{ pr: 1 }}/>
						{record && `${record.name}`} - Reboot...
					</Typography>
				</DialogTitle>

				<DialogContent>
					<Typography variant='body2' color='error'>
						Are you sure you want to reboot the device?
					</Typography>
				</DialogContent>

				<DialogActions>
					<Button label='YES' onClick={handleClick} />
					<Button label='NO' onClick={handleClose} />
				</DialogActions>
			</Dialog>
		</Box>
	);
}

export default RebootButton;
