import { useGetList, TextInput, maxLength } from 'react-admin';
import FormGroup from '@mui/material/FormGroup';

const SettingsInput = (props: any) => {
	const { type } = props;
	const { data, isLoading } = useGetList('properties',
		{
			pagination: { page: 1, perPage: 100 },
			sort: { field: 'name', order: 'ASC'},
			meta: { type: type }
		}
	);

	if (isLoading) return null;

	return (
		<>
		{data && data.map(item =>
			<FormGroup key={item.id} row>
				<TextInput sx={{ minWidth: '20em' }}
					source={`settings.${item.name}`}
					label={item.label}
					helperText={item.description}
					validate={[maxLength(50)]}
				/>
			</FormGroup>
		)}
		</>
	);
};

export default SettingsInput;
