import React, { Component } from 'react';
import { withStyles } from '@material-ui/core/styles';
import TextField from '@material-ui/core/TextField';
import IconButton from '@material-ui/core/IconButton';
import InputAdornment from '@material-ui/core/InputAdornment';
import VisibilityIcon from '@material-ui/icons/Visibility';
import VisibilityOffIcon from '@material-ui/icons/VisibilityOff';
import { addField } from 'react-admin';

const styles = theme => ({
    testInput: {
        minWidth: theme.spacing.unit * 32
    }
});

class PasswordInput extends Component {
    state = {
        showPassword: false,
    };
    
    handleMouseDownPassword = event => {
        event.preventDefault();
    };
    
    handleClickShowPassword = () => {
        this.setState(state => ({ showPassword: !state.showPassword }));
    };

    render() {
        const { input, label, meta: { touched, error }, fullWidth, margin, classes } = this.props;
        const { showPassword } = this.state;

        return (
            <TextField
                label={label}
                autoComplete='current-password'
                type={showPassword ? 'text' : 'password'}
                error={!!(touched && error)}
                helperText={touched && error}
                {...input}
                fullWidth={fullWidth}
                className={classes.testInput}
                margin={margin}
                InputProps={{
                    endAdornment: 
                    <InputAdornment position="end">
                        <IconButton
                            aria-label="Toggle password visibility"
                            onClick={this.handleClickShowPassword}
                            onMouseDown={this.handleMouseDownPassword}
                        >
                            {this.state.showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                        </IconButton>
                    </InputAdornment>
                }}
            />
        );
    }
};

export default addField(withStyles(styles)(PasswordInput));
