import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import { connect } from 'react-redux';
import compose from 'lodash/flowRight';
//import compose from 'recompose/compose';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogActions from '@material-ui/core/DialogActions';
import Typography from '@material-ui/core/Typography';
import { red } from '@material-ui/core/colors';
import { fade } from '@material-ui/core/styles/colorManipulator';
import Icon from '@material-ui/icons/Autorenew';
import { Button, showNotification, CREATE } from 'react-admin';
import { dataProvider } from '../providers';

const styles = theme => ({
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    button: {
        color: red[500],
        '&:hover': {
            backgroundColor: fade(red[50], 0.5),
            '@media (hover: none)': {
                backgroundColor: 'transparent',
            },
        }
    }
});

class RebootButton extends Component {
    constructor(props) {
        super(props);
        this.state = {
            showDialog: false
        }
    }

    handleOpen = () => {
        this.setState({showDialog: true});
    };

    handleClose = () => {
        this.setState({showDialog: false});
    };

    handleClick = () => {
        const {showNotification, record} = this.props;
        this.setState({ showDialog: false });

        dataProvider(CREATE, 'commands', {
            data: { cubeID: record.id, command: 'reboot', deviceType: 'e' }
        })
        .then(() => {
            showNotification('Reboot in process... Wait 1 minute and check the status!', 'warning');
        })
        .catch((e) => {
            showNotification(`Error: ${e}`, 'warning')
        });
    };

    render() {
        const { classes, record } = this.props;
		const { showDialog } = this.state;

        return (
            record
            ?
            <span>
                <Button 
                    disabled={record.online === false}
                    label='Reboot...'
                    className={classes.button}
                    onClick={this.handleOpen}
                >
                    <Icon />
                </Button>
                <Dialog
                    open={showDialog}
                    keepMounted
                    onClose={this.handleClose}
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
                        <Button label='YES' onClick={this.handleClick} />
                        <Button label='NO' onClick={this.handleClose} />
                    </DialogActions>
                </Dialog>
            </span>
            :
            null
        );
    }
}

const enhance = compose(
    connect(null, {
        showNotification
    }),
    withStyles(styles)
);

export default enhance(RebootButton);
