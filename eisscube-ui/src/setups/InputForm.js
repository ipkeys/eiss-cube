import React, { Component, Fragment } from 'react';
import compose from 'recompose/compose';
import { connect } from 'react-redux';
import { reset, submit, isPristine, isSubmitting, formValueSelector } from 'redux-form';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    NumberInput,
    TextInput
} from 'react-admin';

import { SetupFormButton } from './SetupCube';

const styles = theme => ({
    inline: { 
        display: 'inline-block',
        marginRight: theme.spacing.unit * 2, 
        minWidth: theme.spacing.unit * 32   
    }
});

class InputForm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: props.data
        };

        this.handleSave = this.handleSave.bind(this);
    }

    componentDidUpdate(prevProps) {
		if (this.props.data !== prevProps.data) {
			let data = this.props.data; 

			this.setState({
				data
			});	
		}
	}

    handleSave = () => {
        const { submit } = this.props;
        submit('SetupCubeWizard');
    };

    render() {
        const { classes, onSubmit, isPristine, isSubmitting, step, back, next, isInputConnected, inputSignalType } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                form='SetupCubeWizard'
                defaultValue={data}
                onSubmit={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='input.connected' className={classes.inline} margin='dense'/>
                {isInputConnected &&
                    <Fragment>
                        <SelectInput label='Signal type' source="input.signal" choices={[
                            { id: 'P', name: 'Pulses' },
                            { id: 'C', name: 'Cycles' }
                        ]} className={classes.inline} margin='dense' />
                        {inputSignalType && inputSignalType === "P" &&
                            <NumberInput label='Pulse factor (pulses per kWh)' source='input.factor' className={classes.inline} margin='dense'/>
                        }
                        {inputSignalType && inputSignalType === "C" &&
                            <SelectInput label='Watch' source="input.watch" choices={[
                                { id: 1, name: 'Relay 1' },
                                { id: 2, name: 'Relay 2' }
                            ]} className={classes.inline} margin='dense' />
                        }
                        <TextInput label='Label' source='input.label' fullWidth margin='dense'/>
                        <TextInput label='Description' source='input.description' fullWidth margin='dense'/>
                    </Fragment>
                }
                <SetupFormButton step={step} onSave={this.handleSave} onNext={next} onBack={back} pristine={isPristine} submitting={isSubmitting}/>
            </SimpleForm>
        );
    }

}

InputForm.propTypes = {
    data: PropTypes.object
};

const selector = formValueSelector('SetupCubeWizard');

const mapStateToProps = state => ({
	isInputConnected: selector(state, 'input.connected'),
	inputSignalType: selector(state, 'input.signal'),
    isSubmitting: isSubmitting('SetupCubeWizard')(state),
    isPristine: isPristine('SetupCubeWizard')(state)
});

const mapDispatchToProps = {
	reset,
    submit
};

const enhance = compose(
    withStyles(styles)
);

export default enhance(
	connect(mapStateToProps, mapDispatchToProps)(
    	InputForm
	)
);
