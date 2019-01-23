import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Icon from '@material-ui/icons/Settings';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogActions from '@material-ui/core/DialogActions';
import CancelIcon from '@material-ui/icons/Close';

import SetupCube from '../setups/SetupCube';

export const SetupIcon = Icon;

const styles = theme => ({
	btnPadding: {
        paddingRight: theme.spacing.unit
    },
    title: {
        display: 'inline-flex',
        alignItems: 'center',
    },
    content: {
		padding: 0
	}
});

class SetupButton extends Component {
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

    render() {
        const { classes, record } = this.props;
		const { showDialog } = this.state;

        return (
            record
            ?
            <span>
                <Button color="primary" onClick={this.handleOpen} >
                    <SetupIcon className={classes.btnPadding} />
                    Setup
                </Button>
				<Dialog
					fullWidth
					maxWidth='md'								
                    open={showDialog}
                    scroll={'paper'}
					onClose={this.handleClose}
					disableBackdropClick={true}
					aria-labelledby='setup-dialog-title'
				>
					<DialogTitle id='setup-dialog-title'>
						<span className={classes.title}>
                            <SetupIcon className={classes.btnPadding} />
                            {record && `${record.deviceID}`} - Setup Connections
						</span>
					</DialogTitle>
					
					<DialogContent className={classes.content}>
                        <SetupCube deviceID={record.deviceID} />
					</DialogContent>

					<DialogActions>
						<Button onClick={this.handleClose} color='primary'>
 				           <CancelIcon className={classes.btnPadding} />
				            Close
        				</Button>
                    </DialogActions>
				</Dialog>
            </span>
            :
            null
        );
    }
}

export default withStyles(styles)(SetupButton);
