import React from 'react';
import { withStyles } from '@material-ui/core/styles';
import Divider from '@material-ui/core/Divider';

const styles = theme => ({
    divider: {
        marginTop: theme.spacing(1)
	}
});

const DividerField = ({ classes }) => (
	<Divider className={classes.divider} />
);

export default withStyles(styles)(DividerField);
