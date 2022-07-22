import { useState } from 'react';
import {
	Button,
	useRecordContext,
} from 'react-admin';
import {
	Box,
	Typography,
	Dialog,
	DialogTitle,
	DialogContent,
	DialogActions
} from '@mui/material';
import CellIcon from '@mui/icons-material/NetworkCell';
import CellSignal0Icon from '@mui/icons-material/SignalCellular0Bar';
import CellSignal1Icon from '@mui/icons-material/SignalCellular1Bar';
import CellSignal2Icon from '@mui/icons-material/SignalCellular2Bar';
import CellSignal3Icon from '@mui/icons-material/SignalCellular3Bar';
import CellSignal4Icon from '@mui/icons-material/SignalCellular4Bar';
import TestIcon from '@mui/icons-material/NetworkCheck';
import CancelIcon from '@mui/icons-material/Close';
import { green, red, blue, grey, orange } from '@mui/material/colors';
import TestCube from '../test/TestCube';

const SignalStrength = (props: any) => {
	const record = useRecordContext(props);

	switch (record.signalStrength) {
		case 1:
			return <CellSignal0Icon sx={{pl: 1, color: grey[500]}} />;
		case 2:
			return <CellSignal1Icon sx={{pl: 1, color: red[500]}} />;
		case 3:
			return <CellSignal2Icon sx={{pl: 1, color: orange[500]}} />;
		case 4:
			return <CellSignal3Icon sx={{pl: 1, color: blue[500]}} />;
		case 5:
			return <CellSignal4Icon sx={{pl: 1, color: green[500]}} />;
		default:
			return <CellIcon sx={{pl: 1}} />
	}
};

const TestButton = () => {
	const [showDialog, setShowDialog] = useState<boolean>(false);
	const record = useRecordContext();

	if (!record) return null;

	const handleOpen = () => {
		setShowDialog(true);
	};

	const handleClose = () => {
		setShowDialog(false);
	};

	return (
		<Box>
			<Button
				disabled={record.online === false}
				label='Test'
				onClick={handleOpen}
			>
				<TestIcon />
			</Button>

			<Dialog
				fullWidth
				maxWidth='md'
				open={showDialog}
				scroll={'paper'}
				onClose={handleClose}
				aria-labelledby='test-dialog-title'
			>
				<DialogTitle id='test-dialog-title'>
					<Typography sx={{display: 'inline-flex'}}>
						<TestIcon sx={{pr: 1}}/>
						{record && `${record.name}`} - Live test
					</Typography>
					<Typography sx={{display: 'inline-flex', alignItems: 'center', float: 'right'}}>
						Signal Strength - {record.signalStrength} of 5
						{<SignalStrength />}
					</Typography>
				</DialogTitle>

				<DialogContent>
					<TestCube />
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

export default TestButton;
