import React, { Component, Fragment } from 'react';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    NumberInput,
    TextInput,
    FormDataConsumer
} from 'react-admin';
import { FormSpy } from 'react-final-form';

import { SetupFormButton } from './SetupCube';

const styles = theme => ({
});

const InputSettings = ({ formData }) => {
    return (
        formData && formData.input && formData.input.connected ?
        <Fragment>
            <SelectInput label='Signal type' source="input.signal" choices={[
                { id: 'p', name: 'Pulses' },
                { id: 'c', name: 'Cycles' }
            ]} fullWidth />
            {formData.input.signal && formData.input.signal === "p" &&
                <NumberInput label='Factor (Pulses per kWh)' source='input.factor' fullWidth />
            }
            {formData.input.signal && formData.input.signal === "c" &&
                <Fragment>
                    <SelectInput label='Watch' source="input.watch" choices={[
                        { id: 'r', name: 'Raising edge' },
                        { id: 'f', name: 'Falling edge' }
                    ]} fullWidth />

                    <NumberInput label='Load value (kW)' source='input.load' fullWidth />
                </Fragment>
            }
            <TextInput label='Label' source='input.label' fullWidth />
            <TextInput label='Description' source='input.description' fullWidth/>
        </Fragment>
        : null
    );
};

class InputForm extends Component {

    constructor(props) {
        super(props);
        this.state = {
            data: props.data
        };
    }

    componentDidUpdate(prevProps) {
		if (this.props.data !== prevProps.data) {
			let data = this.props.data; 

			this.setState({
				data
			});	
		}
	}

    render() {
        const { onSubmit, step, back, next } = this.props
        const { data } = this.state;

        return (
            <SimpleForm
                initialValues={data}
                save={onSubmit}
                toolbar={null}
            >
                <BooleanInput label='Connected' source='input.connected' />
                <FormDataConsumer>
                    {formDataProps => <InputSettings {...formDataProps} /> }
                </FormDataConsumer>

                <FormSpy subscription={{ pristine: true, submitting: true }}>
                {props => (
                    <SetupFormButton step={step} onNext={next} onBack={back} pristine={props.pristine} submitting={props.submitting}/>
                )}
                </FormSpy>
            </SimpleForm>
        );
    }

}

InputForm.propTypes = {
    data: PropTypes.object
};

export default withStyles(styles)(InputForm);
