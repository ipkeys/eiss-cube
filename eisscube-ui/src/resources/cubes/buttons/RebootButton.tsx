import { useState } from 'react';
import { makeStyles, Theme } from '@material-ui/core/styles';
import {
    Button,
    useDataProvider,
    useRecordContext,
    useNotify
} from 'react-admin';
import { 
    Typography, 
    Dialog, 
    DialogTitle, 
    DialogContent, 
    DialogContentText, 
    DialogActions 
} from '@material-ui/core';
import { red } from '@material-ui/core/colors';
import { alpha } from '@material-ui/core/styles/colorManipulator';
import Icon from '@material-ui/icons/Autorenew';

const useStyles = makeStyles((theme: Theme) => ({ 
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    button: {
        color: red[500],
        '&:hover': {
            backgroundColor: alpha(red[50], 0.5),
            '@media (hover: none)': {
                backgroundColor: 'transparent',
            },
        }
    }
}));


const RebootButton = (props: any) => {
    const [showDialog, setShowDialog] = useState<boolean>(false);
    const classes = useStyles();
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
            notify('Reboot in process... Wait 1 minute and check the status!', 'warning');
        })
        .catch((e) => {
            notify(`Error: ${e}`, 'warning')
        });
    };

    return (
        record
        ?
        <span>
            <Button 
                disabled={record.online === false}
                label='Reboot...'
                className={classes.button}
                onClick={handleOpen}
            >
                <Icon />
            </Button>
            <Dialog
                open={showDialog}
                keepMounted
                onClose={handleClose}
                aria-labelledby='reboot-dialog-title'
                aria-describedby='reboot-dialog-description'
            >
                <DialogTitle id='reboot-dialog-title'>
                    Rebooting...
                </DialogTitle>
                <DialogContent>
                    <DialogContentText id='reboot-dialog-description'>
                        <Typography variant='body2' color='error'>
                            Are you sure you want to reboot the device?
                        </Typography>
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button label='YES' onClick={handleClick} />
                    <Button label='NO' onClick={handleClose} />
                </DialogActions>
            </Dialog>
        </span>
        :
        null
    );
}

export default RebootButton;
