import PropTypes from 'prop-types';
import {
    SimpleForm,
    BooleanInput,
    SelectInput,
    TextInput,
    FormDataConsumer
} from 'react-admin';
import { FormSpy } from 'react-final-form';

import { SetupFormButton } from './SetupCube';

const RelaySettings = ({formData}: {formData: any}) => {
    return (
        formData && formData.relay && formData.relay.connected ?
        <>
            <SelectInput label='To contacts' source='relay.contacts' choices={[
                { id: 'NO', name: 'Normal Open' },
                { id: 'NC', name: 'Normal Close' }
            ]} fullWidth />
            <TextInput label='Label' source='relay.label' fullWidth />
            <TextInput label='Description' source='relay.description' fullWidth />
        </>
        : null
    );
};

const RelayForm = (props: any) => {
    const { data, onSubmit, step, back, next } = props

    return (
        <SimpleForm
            initialValues={data}
            save={onSubmit}
            // @ts-ignore
            toolbar={null}
        >
            <BooleanInput label='Connected' source='relay.connected' />

            <FormDataConsumer>
                {formDataProps => <RelaySettings {...formDataProps} /> }
            </FormDataConsumer>

            <FormSpy subscription={{ pristine: true, submitting: true }}>
            {(p: any) => (
                <SetupFormButton step={step} onNext={next} onBack={back} pristine={p.pristine} submitting={p.submitting}/>                
            )}
            </FormSpy>
        </SimpleForm>
    );

}

RelayForm.propTypes = {
    data: PropTypes.object,
    step: PropTypes.number,
    onSubmit: PropTypes.func,
    next: PropTypes.func,
    back: PropTypes.func
};

export default RelayForm;
