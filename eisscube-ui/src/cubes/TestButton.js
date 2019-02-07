import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import DialogContent from '@material-ui/core/DialogContent';
import DialogActions from '@material-ui/core/DialogActions';
import TestIcon from '@material-ui/icons/NetworkCheck';
import CancelIcon from '@material-ui/icons/Close';

import { Button } from 'react-admin';
import TestCube from '../test/TestCube';

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

class TestButton extends Component {
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
                <Button label='Test' onClick={this.handleOpen} >
                    <TestIcon />
                </Button>
				<Dialog
					fullWidth
					maxWidth='md'								
                    open={showDialog}
                    scroll={'paper'}
					onClose={this.handleClose}
					disableBackdropClick={true}
					aria-labelledby='test-dialog-title'
				>
					<DialogTitle id='test-dialog-title'>
						<span className={classes.title}>
                            <TestIcon className={classes.btnPadding} />
                            {record && `${record.name}`} - Live test
						</span>
					</DialogTitle>
					
					<DialogContent className={classes.content}>
                        <TestCube cubeID={record.id} startTime={new Date()}/>
					</DialogContent>

					<DialogActions>
						<Button label='Close' onClick={this.handleClose} >
 				           <CancelIcon />
        				</Button>
                    </DialogActions>
				</Dialog>
            </span>
            :
            null
        );
    }
}

export default withStyles(styles)(TestButton);
