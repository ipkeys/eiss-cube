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

import CellIcon from '@material-ui/icons/NetworkCell';
import CellSignal0Icon from '@material-ui/icons/SignalCellular0Bar';
import CellSignal1Icon from '@material-ui/icons/SignalCellular1Bar';
import CellSignal2Icon from '@material-ui/icons/SignalCellular2Bar';
import CellSignal3Icon from '@material-ui/icons/SignalCellular3Bar';
import CellSignal4Icon from '@material-ui/icons/SignalCellular4Bar';
import { green, red, blue, grey, orange } from '@material-ui/core/colors';

const styles = theme => ({
	btnPadding: {
        paddingRight: theme.spacing(1)
    },
	signalPadding: {
        paddingLeft: theme.spacing(1)
    },
    title: {
        display: 'inline-flex',
        alignItems: 'center'
    },
    titleRight: {
        float: 'right',
        display: 'inline-flex',
        alignItems: 'center'
    },
    content: {
        paddingLeft: theme.spacing(3),
        paddingRight: theme.spacing(3),
        paddingBottom: 0
	}
});

const getCellIcon = (classes, ss) => {
    switch (ss) {
        case 1:
            return <CellSignal0Icon className={classes.signalPadding} style={{color: grey[500]}} />;
        case 2:
            return <CellSignal1Icon className={classes.signalPadding} style={{color: red[500]}} />;
        case 3:
            return <CellSignal2Icon className={classes.signalPadding} style={{color: orange[500]}} />;
        case 4:
            return <CellSignal3Icon className={classes.signalPadding} style={{color: blue[500]}} />;
        case 5:
            return <CellSignal4Icon className={classes.signalPadding} style={{color: green[500]}} />;
        default:
            return <CellIcon className={classes.signalPadding} />
    }
};

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
                <Button 
                    disabled={record.online === false} 
                    label='Test' 
                    onClick={this.handleOpen} 
                >
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
						<span className={classes.titleRight}>
                            Signal Strength - {record.signalStrength} of 5
                            {getCellIcon(classes, record.signalStrength)}
                        </span>
					</DialogTitle>
					
					<DialogContent className={classes.content}>
                        <TestCube cubeID={record.id} deviceType={record.deviceType}/>
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
