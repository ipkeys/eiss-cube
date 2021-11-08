import { useState } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import { 
    Button,
    useRecordContext 
} from 'react-admin';
import { 
    Dialog, 
    DialogTitle,
    DialogContent,
    DialogActions
} from '@material-ui/core';
import SetupIcon from '@material-ui/icons/Settings';
import CancelIcon from '@material-ui/icons/Close';

import SetupCube from '../setup/SetupCube';

const useStyles = makeStyles((theme: Theme) => ({ 
	btnPadding: {
        paddingRight: theme.spacing(1)
    },
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    content: {
		padding: 0
	}
}));

const SetupButton = (props: any) => {
    const [showDialog, setShowDialog] = useState<boolean>(false);
    const classes = useStyles();
    const record = useRecordContext(props);

    const handleOpen = () => {
        setShowDialog(true);
    };

    const handleClose = () => {
        setShowDialog(false);
    };

    return (
        record
        ?
        <span>
            <Button label='Setup' onClick={handleOpen} >
                <SetupIcon />
            </Button>
            <Dialog
                fullWidth
                maxWidth='sm'								
                open={showDialog}
                scroll={'paper'}
                onClose={handleClose}
                disableBackdropClick={true}
                aria-labelledby='setup-dialog-title'
            >
                <DialogTitle id='setup-dialog-title'>
                    <span className={classes.title}>
                        <SetupIcon className={classes.btnPadding} />
                        {record && `${record.name}`} - Setup Connections
                    </span>
                </DialogTitle>
                
                <DialogContent className={classes.content}>
                    <SetupCube cubeID={record.id} deviceType={record.deviceType} />
                </DialogContent>

                <DialogActions>
                    <Button label='Close' onClick={handleClose} >
                        <CancelIcon />
                    </Button>
                </DialogActions>
            </Dialog>
        </span>
        :
        null
    );
}

export default SetupButton;
