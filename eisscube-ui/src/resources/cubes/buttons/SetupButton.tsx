import { useState } from 'react';
import {
	Button,
	useRecordContext
} from 'react-admin';
import {
	Box,
	Typography,
	Dialog,
	DialogTitle,
	DialogContent,
	DialogActions
} from '@mui/material';
import SetupIcon from '@mui/icons-material/Settings';
import CancelIcon from '@mui/icons-material/Close';
import SetupCube from '../setup/SetupCube';

const SetupButton = (props: any) => {
	const [showDialog, setShowDialog] = useState<boolean>(false);
	const record = useRecordContext(props);

	const handleOpen = () => {
		setShowDialog(true);
	};

	const handleClose = () => {
		setShowDialog(false);
	};

	return (
		record &&
		<Box>
			<Button label='Setup' onClick={handleOpen} >
				<SetupIcon />
			</Button>

			<Dialog
				fullWidth
				maxWidth='sm'
				open={showDialog}
				scroll={'paper'}
				onClose={handleClose}
				aria-labelledby='setup-dialog-title'
			>
				<DialogTitle id='setup-dialog-title'>
					<Typography sx={{ display: 'inline-flex' }}>
						<SetupIcon sx={{ pr: 1 }}/>
						{record && `${record.name}`} - Setup Connections
					</Typography>
				</DialogTitle>

				<DialogContent>
					<SetupCube cubeID={record.id} deviceType={record.deviceType} />
				</DialogContent>

				<DialogActions>
					<Button label='Close' onClick={handleClose} >
						<CancelIcon />
					</Button>
				</DialogActions>
			</Dialog>
		</Box>
	);
}

export default SetupButton;
