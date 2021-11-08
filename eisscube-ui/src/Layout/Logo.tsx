import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles(theme => ({
    logo: {
        background: 'url(Logo.png)',
        backgroundRepeat: 'no-repeat',
        backgroundPosition: 'center',
        minHeight: theme.spacing(10),
        marginTop: theme.spacing(2)  
    }
}));

const Logo = () => {
    const classes = useStyles();
    return (
        <div className={classes.logo} />
    );
}

export default Logo;
