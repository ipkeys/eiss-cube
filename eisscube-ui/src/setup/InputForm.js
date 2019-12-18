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
        const { onSubmit, isPristine, isSubmitting, step, back, next, isInputConnected, inputSignalType } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                form='SetupCubeWizard'
                defaultValue={data}
                onSubmit={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='input.connected' margin='dense'/>
                {isInputConnected &&
                    <Fragment>
                        <SelectInput label='Signal type' source="input.signal" choices={[
                            { id: 'P', name: 'Pulses' },
                            { id: 'C', name: 'Cycles' }
                        ]} margin='dense' fullWidth />
                        {inputSignalType && inputSignalType === "P" &&
                            <NumberInput label='Pulse factor (pulses per kWh)' source='input.factor' margin='dense' fullWidth />
                        }
                        {inputSignalType && inputSignalType === "C" &&
                            <SelectInput label='Watch' source="input.watch" choices={[
                                { id: 'r', name: 'Raising edge' },
                                { id: 'f', name: 'Falling edge' }
                            ]} margin='dense' fullWidth />
                        }
                        <TextInput label='Label' source='input.label' margin='dense' fullWidth />
                        <TextInput label='Description' source='input.description' margin='dense' fullWidth/>
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
