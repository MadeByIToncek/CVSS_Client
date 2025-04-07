package space.itoncek.cvss.client.api.objects;

import java.util.List;

public record Team(int id, String name, String colorDark, String colorBright,List<String> members) {
}
