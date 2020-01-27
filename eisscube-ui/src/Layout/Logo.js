import React from 'react';

import {createStyles, withStyles} from '@material-ui/core/styles';

const styles = (theme) => createStyles({
    logo: {
        background: 'url(/eiss-logo-sm.png)',
        backgroundRepeat: 'no-repeat',
        minHeight: theme.spacing(8),
        marginTop: theme.spacing(3),
        marginLeft: theme.spacing(11),
        marginRight: theme.spacing(11)
      },
});

const Logo = (props) => {
    const {classes} = props;

    return (
        <div className={classes.logo} />
    )
}

export default withStyles(styles)(Logo);