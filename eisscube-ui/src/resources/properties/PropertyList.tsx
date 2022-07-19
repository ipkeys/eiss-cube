import { useMediaQuery, Theme } from '@mui/material';
import {
	downloadCSV,
	Datagrid,
	Filter,
	List,
	SearchInput,
	SelectInput,
	SelectField,
	ShowButton,
	TextField,
	SimpleList,
	usePermissions
} from 'react-admin';
import jsonExport from 'jsonexport/dist';
import { PropertyTypes } from '../common';
import { isSuper } from '../common/Roles';

const PropertyListFilter = (props: any) => (
	<Filter {...props}>
		<SearchInput source='q' alwaysOn />
		<SelectInput source='type' choices={PropertyTypes}/>
	</Filter>
);

const exportPropertyList = (data: any) => {
	const records = data.map((r: any) => ({
		type: PropertyTypes.find(type => type.id === r.type)?.name,
		name: r.name,
		label: r.label,
		description: r.description
	}));

	jsonExport(records, {
		headers: ['type', 'name', 'label', 'description']
		}, (_err, csv) => {
			downloadCSV(csv, 'Custom Properties');
		}
	);
};

const PropertyList = (props: any) => {
	const isSmall = useMediaQuery<Theme>(theme => theme.breakpoints.down('sm'));
	const { permissions } = usePermissions();
	const { bulkActionButtons } = props;

	return (
		<List title='Custom Properties' {...props}
			filters={<PropertyListFilter />}
			sort={{ field: 'name', order: 'ASC' }}
			perPage={10}
			exporter={exportPropertyList}
			{...(isSuper(permissions) ? {bulkActionButtons: bulkActionButtons} : {bulkActionButtons: false})}
		>
			{isSmall ? (
				<SimpleList
					linkType="show"
					primaryText={record => record.label}
					secondaryText={record => record.description}
					tertiaryText={record => record.name}
				/>
			) : (
				<Datagrid
					sx={{
						'& .RaDatagrid-rowOdd': {
							backgroundColor: theme => theme.palette.grey[50],
						},
					}}
				>
					<TextField source='name' label='Name' />
					<TextField source='label' label='Label' />
					<TextField source='description' label='Description' />
					<SelectField source='type' choices={PropertyTypes} />
					<ShowButton />
				</Datagrid>
			)}
		</List>
	);
}

export default PropertyList;
