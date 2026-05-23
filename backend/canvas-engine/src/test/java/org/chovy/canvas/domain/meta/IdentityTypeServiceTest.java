package org.chovy.canvas.domain.meta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityTypeServiceTest {

    @Mock
    private IdentityTypeMapper identityTypeMapper;

    @Mock
    private UserTagCurrentMapper userTagCurrentMapper;

    @Test
    void create_appliesDefaults_and_normalizesCode() {
        IdentityTypeService service = new IdentityTypeService(identityTypeMapper, userTagCurrentMapper);
        IdentityType body = new IdentityType();
        body.setCode("  EMAIL_Address ");
        body.setName("Email");

        IdentityType created = service.create(body);

        ArgumentCaptor<IdentityType> captor = ArgumentCaptor.forClass(IdentityType.class);
        verify(identityTypeMapper).insert(captor.capture());
        IdentityType inserted = captor.getValue();
        assertThat(inserted.getCode()).isEqualTo("email_address");
        assertThat(inserted.getName()).isEqualTo("Email");
        assertThat(inserted.getEnabled()).isEqualTo(1);
        assertThat(inserted.getAllowImport()).isEqualTo(1);
        assertThat(inserted.getMultiValue()).isEqualTo(0);
        assertThat(inserted.getPriority()).isEqualTo(100);
        assertThat(inserted.getParticipateMapping()).isEqualTo(0);
        assertThat(created).isSameAs(body);
    }

    @Test
    void requireImportable_rejectsDisabledType() {
        IdentityType disabled = new IdentityType();
        disabled.setCode("email");
        disabled.setName("Email");
        disabled.setEnabled(0);
        disabled.setAllowImport(1);
        when(identityTypeMapper.selectOne(any())).thenReturn(disabled);

        IdentityTypeService service = new IdentityTypeService(identityTypeMapper, userTagCurrentMapper);

        assertThatThrownBy(() -> service.requireImportable(" EMAIL "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identity type");
    }

    @Test
    void listImportable_returnsEnabledImportableTypes() {
        IdentityType first = new IdentityType();
        first.setCode("email");
        IdentityType second = new IdentityType();
        second.setCode("phone");
        List<IdentityType> expected = List.of(first, second);
        when(identityTypeMapper.selectList(any())).thenReturn(expected);

        IdentityTypeService service = new IdentityTypeService(identityTypeMapper, userTagCurrentMapper);

        assertThat(service.listImportable()).containsExactly(first, second);
    }
}
