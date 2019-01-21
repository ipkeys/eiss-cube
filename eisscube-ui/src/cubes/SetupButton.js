import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import Button from '@material-ui/core/Button';
import Icon from '@material-ui/icons/Settings';
import CloseIcon from '@material-ui/icons/Close';
import { cyan } from '@material-ui/core/colors';
//import SetupCubeDialog from '../setups/SetupCubeDialog';

export const SetupIcon = Icon;

const styles = theme => ({
	btnPadding: {
        paddingRight: theme.spacing.unit
    }
});

class SetupButton extends Component {
    constructor(props) {
        super(props);
        this.state = {
            open: false
        }
    }

    handleOpen = () => {
        this.setState({open: true});
    };

    handleClose = () => {
        this.setState({open: false});
    };

    render() {
        const { classes, record } = this.props;

        return (
            record
            ?
            <span>
                <Button color="primary" onClick={this.handleOpen} >
                <SetupIcon color={cyan[500]} className={classes.btnPadding} />
                    Setup
                </Button>

            </span>
            :
            null
        );
    }
}

export default withStyles(styles)(SetupButton);
